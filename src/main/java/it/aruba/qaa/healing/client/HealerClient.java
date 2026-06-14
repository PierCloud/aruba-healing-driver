package it.aruba.qaa.healing.client;

import it.aruba.qaa.healing.model.HealingRequest;
import it.aruba.qaa.healing.model.HealingResponse;

public interface HealerClient {

    HealingResponse heal(HealingRequest request);
}
