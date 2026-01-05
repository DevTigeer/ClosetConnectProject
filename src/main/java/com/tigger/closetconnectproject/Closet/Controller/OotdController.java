package com.tigger.closetconnectproject.Closet.Controller;

import com.tigger.closetconnectproject.Closet.Dto.OotdDtos;
import com.tigger.closetconnectproject.Closet.Service.OotdService;
import com.tigger.closetconnectproject.Security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ootd")
@RequiredArgsConstructor
public class OotdController {

    private final OotdService ootdService;

    @PostMapping
    public ResponseEntity<OotdDtos.Response> save(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody OotdDtos.CreateRequest request
    ) {
        OotdDtos.Response response = ootdService.save(userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OotdDtos.Response>> getMyOotds(
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        List<OotdDtos.Response> ootds = ootdService.findByUserId(userDetails.getUser().getUserId());
        return ResponseEntity.ok(ootds);
    }

    @DeleteMapping("/{ootdId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long ootdId
    ) {
        ootdService.delete(ootdId, userDetails.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }
}
