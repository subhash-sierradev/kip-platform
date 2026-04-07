package com.integration.management.notification.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponse {

    private long allCount;

    private long unreadCount;

    private long readCount;

    private long errorCount;

    private long infoCount;

    private long successCount;

    private long warningCount;
}
