package com.shangdao.phoenix.entity.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.IProjectEntity;
import com.shangdao.phoenix.entity.interfaces.IStateMachineEntity;
import com.shangdao.phoenix.entity.interfaces.ITagEntity;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.service.FileUploadService.OssImage;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "example")
public class Example implements IStateMachineEntity<ExampleLog, ExampleFile, ExampleNotice>, IProjectEntity<ExampleLog, ExampleFile, ExampleNotice>, ITagEntity<ExampleTag> {
    /**
     *
     */
    @Transient
    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne
    @JoinColumn(name = "entity_manager_id")
    private EntityManager entityManager;

    @Column(name = "text")
    private String text;

    @Length(min = 5, max = 100)
    @Column(name = "name")
    private String name;

    @Column(name = "deleted_at")
    @JsonIgnore
    private Date deletedAt;

    @Min(10)
    @Max(100)
    @Column(name = "score")
    private Integer score;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private State state;

    @Transient
    private List<OssImage> uploadFiles;

    @Transient
    private String note;

    @OneToMany(mappedBy = "entity")
    @JsonIgnore
    private Set<ExampleLog> logs;

    @OneToMany(mappedBy = "entity")
    @JsonIgnore
    private Set<ExampleFile> files;

    @OneToMany(mappedBy = "entity")
    @JsonIgnore
    private Set<ExampleNotice> notices;


    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL)
    private Set<ExampleTag> tags;

    @Column(name = "created_at")
    @JsonIgnore
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;


    @Column(name = "last_modified_at")
    private Date lastModifiedAt;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    @ManyToMany
    @JoinTable(name = "example_member", joinColumns = {@JoinColumn(name = "entity_id")}, inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private Set<User> members;

    @ManyToMany
    @JoinTable(name = "example_subscriber", joinColumns = {@JoinColumn(name = "entity_id")}, inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private Set<User> subscribers;

    @ManyToMany
    @JoinTable(name = "example_department", joinColumns = {@JoinColumn(name = "entity_id")}, inverseJoinColumns = {@JoinColumn(name = "department_id")})
    private Set<Department> departments;

    @Override
    public Set<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(Set<Department> departments) {
        this.departments = departments;
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

    @Override
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    public User getManager() {
        return manager;
    }

    @Override
    public Set<User> getMembers() {
        return members;
    }

    @Override
    public Set<User> getSubscribers() {
        return subscribers;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Set<ExampleLog> getLogs() {
        return logs;
    }

    @Override
    public Date getLastModifiedAt() {
        return lastModifiedAt;
    }


    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    public void setLogs(Set<ExampleLog> logs) {
        this.logs = logs;
    }

    @Override
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public User getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }

    public void setSubscribers(Set<User> subscribers) {
        this.subscribers = subscribers;
    }

    public void setLastModifiedAt(Date lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    @Override
    public Set<ExampleTag> getTags() {
        // TODO Auto-generated method stub
        return tags;
    }

    public void setTags(Set<ExampleTag> tags) {
        this.tags = tags;
    }

    @Override
    public Set<ExampleFile> getFiles() {
        return files;
    }

    public void setFiles(Set<ExampleFile> files) {
        this.files = files;
    }

    @Override
    public List<OssImage> getUploadFiles() {
        return uploadFiles;
    }

    public void setUploadFiles(List<OssImage> uploadFiles) {
        this.uploadFiles = uploadFiles;
    }

    @Override
    public Set<ExampleNotice> getNotices() {
        return notices;
    }

    public void setNotices(Set<ExampleNotice> notices) {
        this.notices = notices;
    }


}