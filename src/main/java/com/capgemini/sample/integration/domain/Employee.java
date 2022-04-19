package com.capgemini.sample.integration.domain;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Builder
@RequiredArgsConstructor
@ToString
public class Employee {

    private final int id;

}
