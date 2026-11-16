package uk.gov.dwp.uc.pairtest.exception;

public class InvalidPurchaseException extends RuntimeException {

    /**
     * Constructs a new InvalidPurchaseException with no message.
     */
    public InvalidPurchaseException() {
        super();
    }


    /**
     * Constructs a new InvalidPurchaseException with the specified message.
     *
     * @param msg the detail message explaining the reason for the exception
     */
    public InvalidPurchaseException(String msg) {
        super(msg);
    }
}
