package main.java.higia;

import br.com.douglas444.datastreamenv.common.ConceptClassificationContext;
import br.com.douglas444.dsframework.interceptor.ConsumerOrRunnableInterceptor;

public class HigiaInterceptor {

    public final ConsumerOrRunnableInterceptor<ConceptClassificationContext> NOVELTY_DETECTION_AL_FRAMEWORK;

    public HigiaInterceptor() {

        this.NOVELTY_DETECTION_AL_FRAMEWORK = new ConsumerOrRunnableInterceptor<>();

    }

}
