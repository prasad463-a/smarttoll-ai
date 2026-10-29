package com.smartToll.ai.anpr;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

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
            ANPRRequest request = new ANPRRequest();
            
            // Encode image as Base64 so Python service receives image bytes in cloud and local
            byte[] fileBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            request.setImageBase64(base64Image);

            // Also save temp file as local fallback
            File tempFile = File.createTempFile("anpr_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            request.setImagePath(tempFile.getAbsolutePath());
            
            // Call AI service
            ANPRResponse response = anprService.processImage(request);
            
            // Cleanup temp file
            tempFile.delete();
            
            return response;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to process uploaded image file", e);
        }
    }
}
