package com.santacruzinstruments.ottopi.init;


import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.control.UiCtrlManager;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class HiltModule {

    // FIXME Why do we need this?
    @Singleton
    @Binds
    public abstract CtrlInterface bindLoginRepository(UiCtrlManager navManager);

}
