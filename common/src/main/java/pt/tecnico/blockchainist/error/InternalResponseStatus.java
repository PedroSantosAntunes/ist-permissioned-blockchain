package pt.tecnico.blockchainist.error;

public enum InternalResponseStatus {
    
    OK,
    BAD_USER_FORMAT,
    BAD_WALLET_FORMAT,
    WALLET_ALREADY_EXISTS,
    WALLET_NOT_FOUND,
    USER_NOT_FOUND,
    NOT_AUTHORIZED,
    REMAINING_BALANCE,
    INSUFFICIENT_BALANCE,
    NEGATIVE_AMOUNT,
    DONE_PULLING,
    UNKNOWN;

}

