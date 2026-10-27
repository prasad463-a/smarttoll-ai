package com.smartToll.ai.anpr;

import com.smartToll.ai.anpr.dto.ANPRRequest;
import com.smartToll.ai.anpr.dto.ANPRResponse;

public interface ANPRService {

    ANPRResponse processImage(ANPRRequest request);

}