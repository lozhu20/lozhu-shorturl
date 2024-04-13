package day.happy365.shorturlservice.service.impl;

import day.happy365.shorturlservice.dao.UrlMappingDao;
import day.happy365.shorturlservice.service.AsyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncTaskImpl implements AsyncTaskService {

    @Autowired
    private UrlMappingDao urlMappingDao;

    @Async
    @Override
    public void updateVisitCount(String id) {
        urlMappingDao.updateVisitCount(id);
    }
}
