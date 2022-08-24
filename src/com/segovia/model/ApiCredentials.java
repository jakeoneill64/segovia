package com.segovia.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ApiCredentials(String account, @JsonIgnore String apiKey) {
}
