package ru.vigtech.globalcrashhandler.handler

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import kotlin.system.exitProcess

/**
 * Этот класс нужен для глобальной обработки ошибок.
 * Вместо вылета приложения, он запускает активити и callbackOnGetException.call() - аля коллбек
 *
 * @property[applicationContext]    Контекс .APP - нужен для того, чтоб там заменить дефолтный Thread.UncaughtExceptionHandler
 * @property[defaultHandler]    супер-класс тобишь сам дефолтный обработчик, он нужен, если есть ошибка в нашем обработчике, то вызовется старый
 * @property activityTobeLaunch     ссылка на экземпляр активити, которая будеть запускаться при ошибке
 * @property extensionMethod    дополнительно расширяемое действие перед вызовом activity при возникновении ошибки
 *
 * --------------------
 * Companion
 *
 * @property init() Нужно запускать в Application() она заменяет Thread.getDefaultUncaughtExceptionHandler()
 * @property getThrowableFromIntent(intent: Intent, callbackOnGetException: callbackOnGetException):Throwable получаем значение ошибки из
 */
class GlobalExceptionHandler private constructor(
    private val applicationContext: Context,
    private val defaultHandler:Thread.UncaughtExceptionHandler,
    private val activityTobeLaunch:Class<*>,
    private val extensionMethod:()->Unit = {}
): Thread.UncaughtExceptionHandler{

    /**
     * Интерфейс для функии обратного вызова при возникновении необработанной ошибки,
     * к примеру тут может быть отправка ошибки на сервер
     *
     * @property CallbackOnException коллбек при получении кода ошибки
     */
    interface CallbackOnException{
        fun callbackOnException(exceptionInJson:String?)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            //Функция расширения
            extensionMethod.invoke()
            launchActivity(applicationContext,activityTobeLaunch, e)
            exitProcess(0)
        } catch(er:Exception){
            Log.e(TAG, "uncaughtException: ",er )
            defaultHandler.uncaughtException(t, e)
        }
    }



    private fun launchActivity(applicationContext: Context,
                               activity: Class<*>,
                               exception:Throwable
    ){
        //добавляем ошибку в формат json, т.к из нее удобно будет обратно привести в формат Throwable
        val crashedIntent = Intent(applicationContext, activity).also {
            it.putExtra(INTENT_DATA_NAME, Gson().toJson(exception))
        }
        //Чтоб не было ошибок при вызове новой activity очищаем backstack
        crashedIntent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        )
        crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        applicationContext.startActivity(crashedIntent)
    }




    companion object{
        const val INTENT_DATA_NAME = "Crash"
        private const val TAG = "GlobalExceptionHAndler"

        /**
         *Функция инициализации замены дефолтного обработчика необработанных ошибок
         * @param[applicationContext] Контекс .APP - нужен для того, чтоб там заменить дефолтный Thread.UncaughtExceptionHandler
         * @param activityTobeLaunch  ссылка на экземпляр активити, которая будеть запускаться при ошибке
         * @property extensionMethod  дополнительно расширяемое действие перед вызовом activity при возникновении ошибки
         *---
         * sample
         * class App: Application() {
         *override fun onCreate() {
         *super.onCreate()
         *GlobalExceptionHandler.init(this, CrashActivity::class.java)
         *}
         *}
         */
        fun init(
            applicationContext: Context,
            activityTobeLaunch: Class<*>,
            extensionMethod: () -> Unit = {}
        ){
            val handler = GlobalExceptionHandler(
                applicationContext, Thread.getDefaultUncaughtExceptionHandler() as Thread.UncaughtExceptionHandler, activityTobeLaunch, extensionMethod
            )
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }


        /**
         * Функция для олучения данных ошибки из вызванного активити
         * Её нужно вызывать в OnCreate() методе жизненного цикла
         *
         * @param callbackOnGetException это функция обратного вызова, которая принимает объект Throwable в формате json
         * @return Throwable?, т.к сама ошибка может быть не описана при вызове
         */
        fun getThrowableFromIntent(intent: Intent, callbackOnGetException: CallbackOnException = object:CallbackOnException{
            override fun callbackOnException(exceptionInJson: String?) {
            }
        }):Throwable?{
             try{
                 val exceptionInJson = intent.getStringExtra(INTENT_DATA_NAME)
                 val exception = Gson().fromJson(exceptionInJson, Throwable::class.java)
                try{
                    callbackOnGetException.callbackOnException(exceptionInJson)
                }catch (e:Exception){
                    Log.e(TAG, "Exception in your callback on get Exception: ", e)
                    throw IllegalStateException("Exception in your callback on get Exception")
                }
                Log.d(TAG, "getThrowableFromIntent: $exceptionInJson")
                return exception

            }catch(e:Exception){
                Log.e(TAG, "getThrowableFromIntent: ",e )
                return null
            }
        }


    }


}

