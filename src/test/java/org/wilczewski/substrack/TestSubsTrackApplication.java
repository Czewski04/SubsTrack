package org.wilczewski.substrack;

import org.springframework.boot.SpringApplication;

public class TestSubsTrackApplication {

    public static void main(String[] args) {
        SpringApplication.from(SubsTrackApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
