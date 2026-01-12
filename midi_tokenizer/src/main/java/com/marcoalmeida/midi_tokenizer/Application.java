package com.marcoalmeida.midi_tokenizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the MIDI Tokenizer.
 * This application provides CLI commands for parsing MIDI files and converting them to JSON.
 */
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
