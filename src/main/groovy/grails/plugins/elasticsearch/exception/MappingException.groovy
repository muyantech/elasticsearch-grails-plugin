package grails.plugins.elasticsearch.exception

/**
 * Created by @marcos-carceles on 07/01/15.
 */
class MappingException extends Exception {

    MappingException() {
        super()
    }

    MappingException(String message) {
        super(message)
    }
}
