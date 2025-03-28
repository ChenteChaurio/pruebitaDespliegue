package edu.eci.UniReserva.UniReserva_Backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.eci.UniReserva.UniReserva_Backend.repository.LabRepository;
import edu.eci.UniReserva.UniReserva_Backend.repository.UserRepository;
import edu.eci.UniReserva.UniReserva_Backend.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eci.UniReserva.UniReserva_Backend.model.Reservation;
import edu.eci.UniReserva.UniReserva_Backend.model.enums.ReservationStatus;
import edu.eci.UniReserva.UniReserva_Backend.repository.ReservationRepository;

public class ReservationServiceImplTest {

    private ReservationRepository reservationRepository;
    private LabRepository labRepository;
    private UserRepository userRepository;
    private ReservationServiceImpl reservationServiceImpl;
    private Reservation testReservation;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeEach
    void setUp() {
        reservationRepository = Mockito.mock(ReservationRepository.class);
        labRepository = Mockito.mock(LabRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        reservationServiceImpl = new ReservationServiceImpl(reservationRepository, labRepository, userRepository);

        testReservation = new Reservation(
                "user123",
                "lab01",
                "2025-05-01",
                "10:00",
                "12:00",
                "Project research"
        );
    }

    @Test
    void shouldCreateReservationWhenAvailable() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(true);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation createdReservation = reservationServiceImpl.createReservation(new Reservation("user123", "lab01", "2025-05-01", "10:00", "12:00", "Project research"));

        assertNotNull(createdReservation);
        assertEquals(testReservation.getLabId(), createdReservation.getLabId());
        assertEquals(testReservation.getDate(), createdReservation.getDate());
        assertEquals(testReservation.getStartTime(), createdReservation.getStartTime());
        assertEquals(testReservation.getEndTime(), createdReservation.getEndTime());
        assertEquals(ReservationStatus.CONFIRMED, createdReservation.getStatus());

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, times(1)).save(reservationCaptor.capture());
        Reservation savedReservation = reservationCaptor.getValue();

        assertEquals(testReservation.getUserId(), savedReservation.getUserId());
        assertEquals(testReservation.getLabId(), savedReservation.getLabId());
        assertEquals(testReservation.getDate(), savedReservation.getDate());
        assertEquals(testReservation.getStartTime(), savedReservation.getStartTime());
        assertEquals(testReservation.getEndTime(), savedReservation.getEndTime());
        assertEquals(ReservationStatus.CONFIRMED, savedReservation.getStatus());
    }

    @Test
    void shouldAllowReservationWhenSameDateAndTimeButDifferentLab() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(labRepository.existsById("lab02")).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(true);

        when(reservationRepository.findByLabId(testReservation.getLabId())).thenReturn(List.of(testReservation));
        when(reservationRepository.findByLabId("lab02")).thenReturn(Collections.emptyList());

        Reservation secondReservation = new Reservation(
                testReservation.getUserId(),
                "lab02",
                testReservation.getDate(),
                testReservation.getStartTime(),
                testReservation.getEndTime(),
                "Second reservation"
        );

        when(reservationRepository.save(any(Reservation.class))).thenReturn(secondReservation);

        Reservation savedSecondReservation = reservationServiceImpl.createReservation(secondReservation);
        assertNotNull(savedSecondReservation);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, times(1)).save(reservationCaptor.capture());

        Reservation savedReservation = reservationCaptor.getValue();

        assertEquals("lab02", savedReservation.getLabId());
        assertEquals(testReservation.getUserId(), savedReservation.getUserId());
        assertEquals(testReservation.getDate(), savedReservation.getDate());
        assertEquals(testReservation.getStartTime(), savedReservation.getStartTime());
        assertEquals(testReservation.getEndTime(), savedReservation.getEndTime());
        assertEquals(ReservationStatus.CONFIRMED, savedReservation.getStatus());
    }

    @Test
    void shouldAllowReservationWhenPreviousIsCanceled() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(true);

        testReservation.setStatus(ReservationStatus.CANCELED);

        when(reservationRepository.findByLabId(testReservation.getLabId())).thenReturn(List.of(testReservation));

        Reservation newReservation = new Reservation(
                testReservation.getUserId(),
                testReservation.getLabId(),
                testReservation.getDate(),
                testReservation.getStartTime(),
                testReservation.getEndTime(),
                "Rebooking after cancellation"
        );

        when(reservationRepository.save(any(Reservation.class))).thenReturn(newReservation);

        Reservation savedReservation = reservationServiceImpl.createReservation(newReservation);

        assertNotNull(savedReservation);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, times(1)).save(reservationCaptor.capture());

        Reservation capturedReservation = reservationCaptor.getValue();

        assertEquals(testReservation.getUserId(), capturedReservation.getUserId());
        assertEquals(testReservation.getLabId(), capturedReservation.getLabId());
        assertEquals(testReservation.getDate(), capturedReservation.getDate());
        assertEquals(testReservation.getStartTime(), capturedReservation.getStartTime());
        assertEquals(testReservation.getEndTime(), capturedReservation.getEndTime());
        assertEquals(ReservationStatus.CONFIRMED, capturedReservation.getStatus()); // Debe estar confirmada
    }


    @Test
    void shouldNotCreateReservationWhenLabNotExist() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reservationServiceImpl.createReservation(testReservation));

        assertEquals("The lab does not exist", exception.getMessage());
    }

    @Test
    void shouldNotCreateReservationWhenUserNotExist() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reservationServiceImpl.createReservation(testReservation));

        assertEquals("The user does not exist", exception.getMessage());
    }

    @Test
    void shouldNotCreateReservationWhenStartTimeIsBeforeCurrentTime() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(true);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime pastTime = LocalTime.now().minusHours(1);

        Reservation pastReservation = new Reservation(
                "user123",
                "lab01",
                LocalDate.now().toString(),
                pastTime.format(timeFormatter),
                LocalTime.now().plusHours(1).format(timeFormatter),
                "Past reservation"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reservationServiceImpl.createReservation(pastReservation));

        assertEquals("The start time must be in the future. You cannot create a reservation with a past time", exception.getMessage());
    }

    @Test
    void shouldNotCreateReservationWhenDateIsBeforeCurrentDate() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(true);

        Reservation pastDateReservation = new Reservation(
                "user123",
                "lab01",
                LocalDate.now().minusDays(1).toString(), // Fecha en el pasado
                "10:00",
                "12:00",
                "Past date reservation"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reservationServiceImpl.createReservation(pastDateReservation));

        assertEquals("You cannot select a past date for your reservation", exception.getMessage());
    }

    @Test
    void shouldNotCreateReservationIfAlreadyExists() {
        when(labRepository.existsById(testReservation.getLabId())).thenReturn(true);
        when(userRepository.existsById(testReservation.getUserId())).thenReturn(true);

        when(reservationRepository.findByLabId(testReservation.getLabId())).thenReturn(List.of(testReservation));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationServiceImpl.createReservation(testReservation);
        });

        assertEquals("There is already a reservation in the lab selected in the time selected", exception.getMessage());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }


    @Test
    public void shouldReturnReservationsWhenUserHasReservations() {
        String userId = "user123";
        String date1 = LocalDate.now().format(dateFormatter);
        String date2 = LocalDate.now().plusDays(1).format(dateFormatter);
        Reservation res1 = new Reservation(userId, "lab1", date1, "10:00", "11:00", "Study");
        Reservation res2 = new Reservation(userId, "lab2", date2, "12:00", "13:00", "Project");

        when(reservationRepository.findByUserId(userId)).thenReturn(Arrays.asList(res1, res2));

        List<Reservation> result = reservationServiceImpl.getReservationsByUserId(userId);

        assertEquals(2, result.size(), "El usuario debería tener 2 reservas");
        verify(reservationRepository, times(1)).findByUserId(userId);
    }

    @Test
    public void shouldNotReturnReservationsWhenUserHasNoReservations() {
        String userId = "user456";

        when(reservationRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<Reservation> result = reservationServiceImpl.getReservationsByUserId(userId);

        assertTrue(result.isEmpty(), "El usuario no debería tener reservas");
        verify(reservationRepository, times(1)).findByUserId(userId);
    }

    @Test
    void shouldCancelReservationWhenItExist() {
        when(reservationRepository.findById(testReservation.getId())).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(testReservation)).thenReturn(testReservation);

        Reservation result = reservationServiceImpl.cancelReservationByReservationId(testReservation.getId());

        assertNotNull(result);
        assertEquals(testReservation, result);
        assertEquals(ReservationStatus.CANCELED, testReservation.getStatus());
        verify(reservationRepository).save(testReservation);
    }

    @Test
    void shouldNotCancelAReservationWhenItNotExist() {
        when(reservationRepository.findById("123")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationServiceImpl.cancelReservationByReservationId("123");
        });

        assertEquals("Reservation with id 123 not found.", exception.getMessage());
    }

    @Test
    void shouldNotCancelAReservationWhenAlreadyIsCancelled() {
        testReservation.setStatus(ReservationStatus.CANCELED);
        when(reservationRepository.findById("123")).thenReturn(Optional.of(testReservation));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reservationServiceImpl.cancelReservationByReservationId("123");
        });

        assertEquals("This reservation is already cancelled", exception.getMessage());
    }

}

