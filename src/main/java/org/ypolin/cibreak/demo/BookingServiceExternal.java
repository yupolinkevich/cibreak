package org.ypolin.cibreak.demo;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.ypolin.cibreak.handler.CiBreak;

@Service
public class BookingServiceExternal {
    @CiBreak
    public ResponseEntity<String> checkRoomAvailability(int roomId) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (roomId % 2 != 0) {
            throw new ExtServiceUnavailableException("503. Service unavailable");
        }
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }
}
