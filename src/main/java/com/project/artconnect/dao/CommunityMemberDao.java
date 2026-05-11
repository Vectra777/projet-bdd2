package com.project.artconnect.dao;

import com.project.artconnect.model.CommunityMember;
import java.util.List;
import java.util.Optional;

public interface CommunityMemberDao {
    Optional<CommunityMember> findById(Long id);

    List<CommunityMember> findAll();

    void save(CommunityMember member);

    void update(CommunityMember member);

    void delete(String name);
}
