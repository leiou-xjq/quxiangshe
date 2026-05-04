package com.quxiangshe.sync.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.common.AbstractCanalLifeCycle;
import com.alibaba.otter.canal.common.CanalException;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.quxiangshe.sync.config.CanalConfig;
import com.quxiangshe.sync.handler.CanalEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Canal客户端
 * 监听MySQL binlog，实时同步数据变更到ES
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanalClient extends AbstractCanalLifeCycle {

    private final CanalConfig canalConfig;
    private final CanalEventHandler eventHandler;

    private CanalConnector connector;
    private ExecutorService executorService;

    @EventListener(ApplicationReadyEvent.class)
    public void startCanalClient() {
        if (!canalConfig.isEnabled()) {
            log.info("Canal同步已禁用");
            return;
        }

        log.info("启动Canal客户端: server={}, destination={}", 
                canalConfig.getServer(), canalConfig.getDestination());

        try {
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalConfig.getHost(), canalConfig.getPort()),
                    canalConfig.getDestination(),
                    canalConfig.getUsername(),
                    canalConfig.getPassword()
            );

            connector.connect();
            connector.subscribe(canalConfig.getTables());
            connector.rollback();

            executorService = Executors.newFixedThreadPool(canalConfig.getThreads());
            for (int i = 0; i < canalConfig.getThreads(); i++) {
                executorService.submit(this::consume);
            }

            log.info("Canal客户端启动成功");

        } catch (Exception e) {
            log.error("Canal客户端启动失败", e);
        }
    }

    @Async
    public void consume() {
        while (true) {
            try {
                Message message = connector.getWithoutAck(canalConfig.getBatchSize());
                long batchId = message.getId();

                if (message.getEntries() != null && !message.getEntries().isEmpty()) {
                    processEntries(message.getEntries());
                }

                connector.ack(batchId);

            } catch (CanalException e) {
                log.error("Canal消费异常，正在重连...", e);
                try {
                    connector.connect();
                    connector.subscribe(canalConfig.getTables());
                    connector.rollback();
                } catch (Exception reconnectionException) {
                    log.error("重连失败，10秒后重试", reconnectionException);
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                log.error("处理消息异常", e);
            }
        }
    }

    private void processEntries(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }

            try {
                String tableName = entry.getHeader().getTableName();
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    handleRowData(tableName, rowChange.getEventType(), rowData);
                }

            } catch (Exception e) {
                log.error("解析binlog失败: table={}", entry.getHeader().getTableName(), e);
            }
        }
    }

    private void handleRowData(String tableName, CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        switch (tableName) {
            case "t_note":
                handleNoteChange(eventType, rowData);
                break;
            case "user":
                handleUserChange(eventType, rowData);
                break;
            default:
                log.debug("未处理的表: {}", tableName);
        }
    }

    private void handleNoteChange(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        Long noteId = getColumnValue(rowData, "id");
        if (noteId == null) {
            return;
        }

        switch (eventType) {
            case INSERT:
                log.info("笔记新增: noteId={}", noteId);
                eventHandler.handleNoteInsert(noteId);
                break;
            case UPDATE:
                log.info("笔记更新: noteId={}", noteId);
                eventHandler.handleNoteUpdate(noteId);
                break;
            case DELETE:
                log.info("笔记删除: noteId={}", noteId);
                eventHandler.handleNoteDelete(noteId);
                break;
            default:
                log.debug("未处理的笔记事件: {}", eventType);
        }
    }

    private void handleUserChange(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        Long userId = getColumnValue(rowData, "id");
        if (userId == null) {
            return;
        }

        switch (eventType) {
            case INSERT:
                log.info("用户新增: userId={}", userId);
                eventHandler.handleUserInsert(userId);
                break;
            case UPDATE:
                log.info("用户更新: userId={}", userId);
                eventHandler.handleUserUpdate(userId);
                break;
            case DELETE:
                log.info("用户删除: userId={}", userId);
                eventHandler.handleUserDelete(userId);
                break;
            default:
                log.debug("未处理的用户事件: {}", eventType);
        }
    }

    private Long getColumnValue(CanalEntry.RowData rowData, String columnName) {
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                return column.getValue().isEmpty() ? null : Long.parseLong(column.getValue());
            }
        }
        return null;
    }

    public void stop() {
        log.info("停止Canal客户端");
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (connector != null) {
            connector.disconnect();
        }
    }
}
