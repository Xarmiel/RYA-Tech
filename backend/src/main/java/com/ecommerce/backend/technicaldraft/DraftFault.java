package com.ecommerce.backend.technicaldraft;

import java.util.List;

public class DraftFault extends RuntimeException {
    final int status;
    final List<String> issues;
    DraftFault(int status, String message) { this(status, message, List.of()); }
    DraftFault(int status, String message, List<String> issues) { super(message); this.status = status; this.issues = List.copyOf(issues); }
}
