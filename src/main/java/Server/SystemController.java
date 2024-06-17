package Server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ServiceLayer.*;

@RestController
@RequestMapping(path = "/api/system")
public class SystemController {
    private final SystemService _systemService;

    @Autowired
    public SystemController(SystemService systemService) {
        this._systemService = systemService;
    }

    @GetMapping("/openSystem")
    public ResponseEntity<Response> openSystem(@RequestHeader(value = "Authorization", required = true) String token) {
        ResponseEntity<Response> response = _systemService.openSystem(token);
        return response;
    }

    @GetMapping("/enterSystem")
    public ResponseEntity<Response> enterSystem() {
        ResponseEntity<Response> resp = _systemService.requestToEnterSystem();
        return resp;
    }

    @GetMapping("/leaveSystem")
    public ResponseEntity<Response> leaveSystem(@RequestHeader(value = "Authorization", required = true) String token) {
        ResponseEntity<Response> resp = _systemService.leaveSystem(token);
        return resp;
    }

}
