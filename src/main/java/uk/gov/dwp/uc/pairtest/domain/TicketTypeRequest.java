package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */

public class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    // Added price to the ticket type enum
    // this allows new ticket types and change in prices without changing the implementation
    public enum Type {
        ADULT(25),
        CHILD(15),
        INFANT(0);

        private final int price;

        Type(int price) {
            this.price = price;
        }

        /**
         * Returns the price of the ticket type.
         *
         * @return ticket price in pounds
         */
        public int getPrice() {
            return price;
        }
    }
}
