package cz.cvut.kbss.termit.exception;

/**
 * Indicates that an operation has been attempted which is not supported by the particular type of asset.
 */
public class UnsupportedAssetOperationException extends TermItException {

    public UnsupportedAssetOperationException(String message) {
        super(message);
    }

    public UnsupportedAssetOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
