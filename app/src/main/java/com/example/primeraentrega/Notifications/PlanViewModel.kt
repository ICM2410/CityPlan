package com.example.primeraentrega.Notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.io.IOException
import java.time.LocalDateTime

class PlanViewModel: ViewModel() {


    var state = PlanState()
        private set

    private val api: FcmApi= Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create()

    fun onRemoteTokenChange(newToken:String){
        state.remoteToken=newToken
    }

    fun onSubmitNewToken(){
        state.isEnteringToken=false
    }

    fun  onMessageChange(message:String){
        state.messageText=message
    }

    fun  onIdPlanChange(id:String){
        state.idPlan=id
    }

    fun  onTimePlanChange(time: LocalDateTime){
        state.time=time
    }

    fun  onIdAlarmPlanChange(id: Int){
        state.idAlarm=id
    }

    fun sendMessage(isBroadcast: Boolean){
        Log.i("sendmessage","send")
        viewModelScope.launch{
            val message=SendMessageDTO(
                to=if(isBroadcast) null else state.remoteToken,
                notification = NotificationBody(
                    title = "Nuevo plan!",
                    body = state.messageText,
                    id = state.idPlan,
                    alarmId = state.idAlarm,
                    idGrupo = state.idGrupo
                )
            )

            try {
                if (isBroadcast){
                    api.broadcast(message)
                }else{
                    api.sendMessage(message)
                }

                state.messageText=""
            }
            catch (e: HttpException)
            {
                e.printStackTrace()
            }
            catch (e: IOException)
            {
                e.printStackTrace()
            }

        }
    }

    fun onIdGrupoPlanChange(idGrupo: String) {
        state.idGrupo=idGrupo
    }

}