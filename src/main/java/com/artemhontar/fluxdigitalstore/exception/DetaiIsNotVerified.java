package com.artemhontar.fluxdigitalstore.exception;

import java.util.List;
import java.util.Map;

public class DetaiIsNotVerified extends RuntimeException {
    private final Map<String, List<String>> errors;

    public DetaiIsNotVerified(Map<String, List<String>> errors) {
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }


}
