package com.shangdao.phoenix.entity.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.ILog;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;

import javax.persistence.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "example_log")
public class ExampleLog implements ILog<Example, ExampleFile, ExampleNotice> {

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

    @ManyToOne
    @JoinColumn(name = "entity_id")
    private Example entity;

    @Column(name = "create_at")
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "act_id")
    private Act act;

    @Column(name = "name")
    private String name;

    @JoinColumn(name = "before_state_id")
    @ManyToOne
    private State beforeState;

    @JoinColumn(name = "after_state_id")
    @ManyToOne
    private State afterState;

    @Column(name = "difference")
    @Lob
    private String difference;

    @Column(name = "ip")
    private String ip;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "imei")
    private String imei;

    @Column(name = "terminal")
    @Enumerated(EnumType.STRING)
    private Terminal terminal;

    @OneToMany(mappedBy = "log")
    private Set<ExampleFile> files;

    @OneToMany(mappedBy = "log")
    private Set<ExampleNotice> notices;

    @Column(name = "note")
    private String note;

    @Column(name = "deleted_at")
    private Date deletedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private State state;

    @Override
    public Set<ExampleNotice> getNotices() {
        return notices;
    }

    @Override
    public void setNotices(Set<ExampleNotice> notices) {
        this.notices = notices;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
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

    @Override
    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public Set<ExampleFile> getFiles() {
        return files;
    }

    @Override
    public void setFiles(Set<ExampleFile> files) {
        this.files = files;
    }

    @Override
    public User getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getId()
     */
    @Override
    public long getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setId(long)
     */
    @Override
    public void setId(long id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getCreatedAt()
     */
    @Override
    public Date getCreatedAt() {
        // TODO Auto-generated method stub
        return createdAt;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getEntity()
     */
    @Override
    public Example getEntity() {
        // TODO Auto-generated method stub
        return entity;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setEntity(com.shangdao.phoenix.
     * entity.Example)
     */
    @Override
    public void setEntity(Example entity) {
        this.entity = entity;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setCreatedAt(java.util.Date)
     */
    @Override
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getAct()
     */
    @Override
    public Act getAct() {
        // TODO Auto-generated method stub
        return act;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.shangdao.phoenix.entity.loginter#setAct(com.shangdao.phoenix.entity.
     * Act)
     */
    @Override
    public void setAct(Act act) {
        this.act = act;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return name;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getBeforeState()
     */
    @Override
    public State getBeforeState() {
        // TODO Auto-generated method stub
        return beforeState;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getAfterState()
     */
    @Override
    public State getAfterState() {
        // TODO Auto-generated method stub
        return afterState;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getDifference()
     */
    @Override
    public String getDifference() {
        ObjectMapper mapper = new ObjectMapper();
        List<DiffItem> readValue = null;
        try {
            readValue = mapper.readValue(difference, new TypeReference<List<DiffItem>>() {
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return this.difference;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getIp()
     */
    @Override
    public String getIp() {
        // TODO Auto-generated method stub
        return ip;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getLongitude()
     */
    @Override
    public Double getLongitude() {
        // TODO Auto-generated method stub
        return longitude;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getLatitude()
     */
    @Override
    public Double getLatitude() {
        // TODO Auto-generated method stub
        return latitude;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getImei()
     */
    @Override
    public String getImei() {
        // TODO Auto-generated method stub
        return imei;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#getTerminal()
     */
    @Override
    public Terminal getTerminal() {
        // TODO Auto-generated method stub
        return terminal;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * com.shangdao.phoenix.entity.loginter#setBeforeState(com.shangdao.phoenix.
     * entity.State)
     */
    @Override
    public void setBeforeState(State beforeState) {
        this.beforeState = beforeState;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.shangdao.phoenix.entity.loginter#setAfterState(com.shangdao.phoenix.
     * entity.State)
     */
    @Override
    public void setAfterState(State afterState) {
        this.afterState = afterState;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setDifference(java.lang.String)
     */
    @Override
    public void setDifference(String difference) {
        this.difference = difference;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setIp(java.lang.String)
     */
    @Override
    public void setIp(String ip) {
        this.ip = ip;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setLongitude(java.lang.Double)
     */
    @Override
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setLatitude(java.lang.Double)
     */
    @Override
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.shangdao.phoenix.entity.loginter#setImei(java.lang.String)
     */
    @Override
    public void setImei(String imei) {
        this.imei = imei;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.shangdao.phoenix.entity.loginter#setTerminal(com.shangdao.phoenix.
     * util.HTTPHeader.Terminal)
     */
    @Override
    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }


}