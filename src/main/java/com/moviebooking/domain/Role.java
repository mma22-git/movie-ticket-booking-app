package com.moviebooking.domain;

/**
 * Access roles. ADMIN manages the catalog (movies, theaters, screens, shows); CUSTOMER
 * browses and books. Kept intentionally minimal — advanced auth is out of scope.
 */
public enum Role {
    ADMIN,
    CUSTOMER
}
