package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.service.ExhibitionService;

import java.util.List;
import java.util.Optional;

public class JdbcExhibitionService implements ExhibitionService {
    private final ExhibitionDao exhibitionDao;

    public JdbcExhibitionService(ExhibitionDao exhibitionDao) {
        this.exhibitionDao = exhibitionDao;
    }

    @Override
    public List<Exhibition> getAllExhibitions() {
        return exhibitionDao.findAll();
    }

    @Override
    public Optional<Exhibition> getExhibitionByTitle(String title) {
        return exhibitionDao.findAll().stream()
                .filter(exhibition -> exhibition.getTitle() != null && exhibition.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    @Override
    public void createExhibition(Exhibition exhibition) {
        exhibitionDao.save(exhibition);
    }

    @Override
    public void updateExhibition(Exhibition exhibition) {
        exhibitionDao.update(exhibition);
    }

    @Override
    public void deleteExhibition(String title) {
        exhibitionDao.delete(title);
    }
}
