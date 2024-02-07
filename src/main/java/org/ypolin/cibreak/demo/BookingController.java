package org.ypolin.cibreak.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ypolin.cibreak.handler.CiBreaker;

@RestController
@RequestMapping(value = "/booking")
public class BookingController {
    @Autowired
    private BookingServiceExternal bookingServiceExternal;
    @Autowired
    private CiBreaker ciBreaker;
    @GetMapping(value = "rooms/availability/{id}")
    public void checkRoomAvailability(@PathVariable int id) {
        ResponseEntity<String> response = bookingServiceExternal.checkRoomAvailability(id);
        System.out.println(response.getStatusCode());
    }
}
