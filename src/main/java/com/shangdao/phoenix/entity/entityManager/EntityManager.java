package com.shangdao.phoenix.entity.entityManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.interfaces.IBaseEntity;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.tag.Tag;
import com.shangdao.phoenix.entity.user.User;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "entity_manager")
public class EntityManager implements IBaseEntity, Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name", unique = true)
    private String name;
    @Column(name = "has_log")
    private boolean hasLog;
    @Column(name = "has_project")
    private boolean hasProject;
    @Column(name = "has_state_machine")
    private boolean hasStateMachine;
    @Column(name = "has_tag")
    private boolean hasTag;


    @Column(name = "deleted_at")
    @JsonIgnore
    private Date deletedAt;

    @ManyToOne
    @JoinColumn(name = "entity_manager_id")
    private EntityManager entityManager;


    @OneToMany(mappedBy = "entityManager")
    private Set<State> states;
    @OneToMany(mappedBy = "entityManager")
    private Set<Act> acts;
    @OneToMany(mappedBy = "entityManager")
    private Set<Tag> tags;

    @Column(name = "created_at")
    @JsonIgnore
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private State state;

    @Column(name = "manager_group")
    @Enumerated(EnumType.STRING)
    private ManagerGroup managerGroup;

    @Override
    public State getState() {
        return state;
    }

    public ManagerGroup getManagerGroup() {
        return managerGroup;
    }

    public void setManagerGroup(ManagerGroup managerGroup) {
        this.managerGroup = managerGroup;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public User getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Date getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean isHasLog() {
        return hasLog;
    }

    public void setHasLog(boolean hasLog) {
        this.hasLog = hasLog;
    }

    public boolean isHasProject() {
        return hasProject;
    }

    public void setHasProject(boolean hasProject) {
        this.hasProject = hasProject;
    }

    public boolean isHasStateMachine() {
        return hasStateMachine;
    }

    public void setHasStateMachine(boolean hasStateMachine) {
        this.hasStateMachine = hasStateMachine;
    }

    public boolean isHasTag() {
        return hasTag;
    }

    public void setHasTag(boolean hasTag) {
        this.hasTag = hasTag;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public Set<Act> getActs() {
        return acts;
    }

    public void setActs(Set<Act> acts) {
        this.acts = acts;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public enum ManagerGroup {
        DEVELOPER, ADMIN
    }

}
