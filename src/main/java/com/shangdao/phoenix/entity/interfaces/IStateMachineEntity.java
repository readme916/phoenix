package com.shangdao.phoenix.entity.interfaces;

import com.shangdao.phoenix.entity.state.State;

public interface IStateMachineEntity<L extends ILog, F extends IFile, N extends INoticeLog> extends ILogEntity<L, F, N> {
    @Override
    public State getState();
}
