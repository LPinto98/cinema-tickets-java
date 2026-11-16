package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS = 25;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    // constructor for this class
    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validatePurchase(accountId, ticketTypeRequests);

        // Calculate tickets for each type and total amount to pay
        TicketSummary ticketSummary = calculateTickets(ticketTypeRequests);

        int infantTickets = ticketSummary.infants();
        int childrenTickets = ticketSummary.children();
        int adultTickets = ticketSummary.adults();

        // Validate business rule: every infant needs at least one adult lap
        // there should be at least one adult with the children and infant tickets
        validateAdultChildPairing(infantTickets, childrenTickets, adultTickets);

        // Assumption: payment goes through successfully
        ticketPaymentService.makePayment(accountId, ticketSummary.amountToPay());

        // Infants cannot reserve their seat, they use an adult's lap
        int seatsReserved = adultTickets + childrenTickets;
        seatReservationService.reserveSeat(accountId, seatsReserved);
    }

    // verifies valid account id and ticket request
    private void validatePurchase(Long accountId, TicketTypeRequest[] requests) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0 || requests == null || requests.length == 0) {
            throw new InvalidPurchaseException();
        }
    }

    private void validateRequest(TicketTypeRequest ticketTypeRequest) {
        if (ticketTypeRequest == null || ticketTypeRequest.getTicketType() == null) {
            throw new InvalidPurchaseException();
        }

        if (ticketTypeRequest.getNoOfTickets() <= 0) {
            throw new InvalidPurchaseException();
        }
    }

    private void validateAdultChildPairing(int infant, int child, int adult) throws InvalidPurchaseException {
        // If infants exceed adults, or if there are children/infants with zero adults, reject
        if (infant > adult || (child > 0 && adult == 0)) {
            throw new InvalidPurchaseException("Not enough adult tickets purchased");
        }
    }


    /**
     * Iterates through the provided ticket requests, validates individual items,
     * aggregates ticket counts by category, calculates the total cost, and enforces
     * the maximum overall ticket limit (25).
     *
     * @param requests an array of ticket type requests to process
     * @return a TicketSummary containing aggregated counts and the total amount to pay
     * @throws InvalidPurchaseException if any request is invalid, has an unknown type, or exceeds the ticket limit
     */
    private TicketSummary calculateTickets(TicketTypeRequest[] requests) throws InvalidPurchaseException {
        int adults = 0;
        int children = 0;
        int infants = 0;
        int total = 0;
        int amountToPay = 0;

        for (TicketTypeRequest request : requests) {
            // Ensure the individual request object and its attributes are well-formed
            validateRequest(request);

            int count = request.getNoOfTickets();
            TicketTypeRequest.Type type = request.getTicketType();

            // Compute number of tickets for each type
            // throws an exception if the type is not one of the cases
            switch (type) {
                case ADULT  -> adults += count;
                case CHILD  -> children += count;
                case INFANT -> infants += count;
                default     -> throw new InvalidPurchaseException("Invalid Ticket Type");
            }

            // in case of infants -> amountToPay = count * 0
            amountToPay += count * type.getPrice();
            total += count;

            // Maximum 25 tickets in one transaction; MAX_TICKETS value can be changed
            if (total > MAX_TICKETS) {
                throw new InvalidPurchaseException("Maximum tickets limit exceeded");
            }
        }

        // Return the consolidated summary of the transaction
        return new TicketSummary(adults, children, infants, amountToPay);
    }

    private record TicketSummary(int adults, int children, int infants, int amountToPay) {}
}
