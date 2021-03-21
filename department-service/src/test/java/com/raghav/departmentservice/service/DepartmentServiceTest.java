package com.raghav.departmentservice.service;

import com.raghav.departmentservice.entity.Department;
import com.raghav.departmentservice.repository.DepartmentRepository;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DepartmentServiceTest {

    @InjectMocks
    Department department;

    @InjectMocks
    DepartmentRepository repository;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
    }
}
