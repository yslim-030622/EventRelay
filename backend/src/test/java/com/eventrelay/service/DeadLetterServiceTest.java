package com.eventrelay.service;

import com.eventrelay.model.DeadLetterEvent;
import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.model.WebhookSource;
import com.eventrelay.repository.DeadLetterEventRepository;
import com.eventrelay.repository.IncomingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterServiceTest {

    @Mock
    private DeadLetterEventRepository deadLetterEventRepository;

    @Mock
    private IncomingEventRepository incomingEventRepository;

    @Mock
    private EventRoutingService eventRoutingService;

    @Mock
    private DiscordAlertService discordAlertService;

    private DeadLetterService deadLetterService;

    @BeforeEach
    void setUp() {
        deadLetterService = new DeadLetterService(
            deadLetterEventRepository,
            incomingEventRepository,
            eventRoutingService,
            discordAlertService
        );
    }

    @Test
    void moveToDeadLetterReusesExistingRecordForReplayedEvent() {
        IncomingEvent event = new IncomingEvent();
        event.setId(42L);
        event.setEventId("evt-replayed");
        event.setEventType("unknown");
        event.setStatus(EventStatus.FAILED);

        WebhookSource source = new WebhookSource();
        source.setName("custom");
        event.setSource(source);

        DeadLetterEvent existing = new DeadLetterEvent();
        existing.setEvent(event);
        existing.setReplayed(true);
        existing.setReplayedAt(Instant.now());

        when(deadLetterEventRepository.findByEvent(event)).thenReturn(Optional.of(existing));
        when(incomingEventRepository.save(event)).thenReturn(event);
        when(deadLetterEventRepository.save(existing)).thenReturn(existing);

        DeadLetterEvent saved = deadLetterService.moveToDeadLetter(
            event,
            "Processing failed for event 42",
            "Max retries exceeded"
        );

        assertThat(saved).isSameAs(existing);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DEAD_LETTER);
        assertThat(event.getLastError()).isEqualTo("Processing failed for event 42");
        assertThat(existing.getErrorMessage()).isEqualTo("Processing failed for event 42");
        assertThat(existing.getReason()).isEqualTo("Max retries exceeded");
        assertThat(existing.isReplayed()).isFalse();
        assertThat(existing.getReplayedAt()).isNull();
        verify(deadLetterEventRepository).findByEvent(event);
        verify(deadLetterEventRepository).save(existing);
        verify(discordAlertService).sendAlert(any(), any(), any(Integer.class));
    }
}
