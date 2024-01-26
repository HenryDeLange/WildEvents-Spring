package mywild.activity.calculate;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import mywild.activity.Activity;
import mywild.activity.ActivityEntity;

@Slf4j
@Validated
@Service
public class CalculateService {

    public @Valid Activity calculateActivity(@NotNull ActivityEntity activity) {
        doObservationGet(147396);
        return null;
    }

    private void doObservationGet(int taxaId) {
        
    }

}
