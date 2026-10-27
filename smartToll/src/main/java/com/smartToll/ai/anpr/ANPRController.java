package com.smartToll.ai.anpr;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;

import com.smartToll.ai.anpr.dto.ANPRRequest;
import com.smartToll.ai.anpr.dto.ANPRResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/anpr")
@RequiredArgsConstructor
public class ANPRController {

    private final ANPRService anprService;

    @PostMapping(value = "/process", consumes = {"multipart/form-data"})
    public ANPRResponse processImage(@RequestParam("image") MultipartFile file) {
        
        try {
            // Save the uploaded file to a temporary location
            File tempFile = File.createTempFile("anpr_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            
            // Create the request object containing the absolute path for Python to read
            ANPRRequest request = new ANPRRequest();
            request.setImagePath(tempFile.getAbsolutePath());
            
            // Call the service
            ANPRResponse response = anprService.processImage(request);
            
            // Optionally delete the temporary file after processing
            tempFile.delete();
            
            return response;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to process uploaded image file", e);
        }
    }
}