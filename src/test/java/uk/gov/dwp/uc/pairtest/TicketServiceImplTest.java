package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import thirdparty.paymentgateway.*;
import thirdparty.seatbooking.*;
import uk.gov.dwp.uc.pairtest.domain.*;
import uk.gov.dwp.uc.pairtest.exception.*;

import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private static final Long VALID_ACCOUNT_ID = 100L;

    @Test
    void shouldSuccessfullyPurchaseAdultTicketsOnly() throws InvalidPurchaseException {
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, request);

        verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 50); // 2 * £25
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 2);  // 2 seats
    }

    @Test
    void shouldSuccessfullyPurchaseMixedTicketsWithChildrenAndInfants() throws InvalidPurchaseException {
        var adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        var childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        var infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultRequest, childRequest, infantRequest);

        // Payment: (2 * £25) + (2 * £15) + (2 * £0) = £50 + £30 + £0 = £80
        verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 80);

        // Seats: Adults (2) + Children (2) = 4 seats (Infants do not get allocated a seat)
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 4);
    }

    @Test
    void shouldAllowExactMaxTicketsLimitOf25() throws InvalidPurchaseException {
        var request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, request);

        verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 625); // 25 * £25
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 25);
    }

    @ParameterizedTest
    @MethodSource("invalidPurchaseScenariosProvider")
    void shouldRejectInvalidPurchases(Long accountId, TicketTypeRequest[] requests) {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(accountId, requests)
        );

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    private static Stream<Arguments> invalidPurchaseScenariosProvider() {
        return Stream.of(
                // Account ID validation failures
                Arguments.of(null, new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)}),
                Arguments.of(0L, new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)}),

                // Request array validation failures
                Arguments.of(VALID_ACCOUNT_ID, null),
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[0]),
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{null}),

                // Individual request attribute validation failures
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{new TicketTypeRequest(null, 1)}),
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0)}),
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -5)}),

                // Exceeding maximum tickets limit (25)
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6)
                }),

                // Business rule: Child ticket purchased without an adult
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)}),

                // Business rule: Infant ticket purchased without an adult
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)}),

                // Business rule: Infants exceeding adults
                Arguments.of(VALID_ACCOUNT_ID, new TicketTypeRequest[]{
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2)
                })
        );
    }
}
