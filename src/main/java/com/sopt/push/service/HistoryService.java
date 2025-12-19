package com.sopt.push.service;

import static com.sopt.push.common.Constants.HISTORY_ENTITY;
import static com.sopt.push.common.Constants.HISTORY_PREFIX;
import static com.sopt.push.common.Constants.SEPARATOR;

import com.sopt.push.domain.HistoryEntity;
import com.sopt.push.dto.CreateHistoryDto;
import com.sopt.push.repository.HistoryRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class HistoryService {

  private final HistoryRepository historyRepository;

  private static final String DATE_FORMAT_YEAR_MONTH = "yyyy-MM";

  public HistoryService(HistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  public void createLog(CreateHistoryDto createHistoryDto) {

    Instant now = Instant.now();
    DateTimeFormatter yearMonthFormatter =
        DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR_MONTH).withZone(ZoneId.systemDefault());

    String pk = HISTORY_PREFIX + yearMonthFormatter.format(now);
    String sk = HISTORY_PREFIX + now + SEPARATOR + createHistoryDto.transactionId();

    HistoryEntity history = new HistoryEntity();
    history.setPk(pk);
    history.setSk(sk);
    history.setTitle(createHistoryDto.title());
    history.setContent(createHistoryDto.content());
    history.setEntity(HISTORY_ENTITY);
    history.setDeviceToken(createHistoryDto.deviceToken());
    history.setWebLink(createHistoryDto.webLink());
    history.setApplink(createHistoryDto.applink());
    history.setNotificationType(createHistoryDto.notificationType());
    history.setOrderServiceName(createHistoryDto.orderServiceName());
    history.setStatus(createHistoryDto.status());
    history.setAction(createHistoryDto.action());
    history.setPlatform(createHistoryDto.platform());
    history.setCategory(createHistoryDto.category());
    history.setUserIds(createHistoryDto.userIds());
    history.setMessageIds(createHistoryDto.messageIds());
    history.setErrorCode(createHistoryDto.errorCode());
    history.setErrorMessage(createHistoryDto.errorMessag());
    history.setId(createHistoryDto.id());

    historyRepository.save(history);
  }
}
