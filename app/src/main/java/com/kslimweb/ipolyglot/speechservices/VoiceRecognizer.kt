package com.kslimweb.ipolyglot.speechservices

import android.app.Activity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.kslimweb.ipolyglot.MainViewModel
import com.kslimweb.ipolyglot.adapter.SearchResponseAlQuranAdapter
import com.kslimweb.ipolyglot.model.alquran.HitAlQuran
import com.kslimweb.ipolyglot.network.algolia.Searcher
import com.kslimweb.ipolyglot.network.translate.GoogleTranslate
import kotlinx.android.synthetic.main.cardview_speech_translate.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceRecognizer(
    private val mSpeechRecognizer: SpeechRecognizer,
//    private val intent: Intent,
    private val googleTranslate: GoogleTranslate,
    private val searcher: Searcher,
    private val viewModel: MainViewModel,
    private val gson: Gson,
    private val rvSearch: RecyclerView
) : RecognitionListener {

    // SearchResponseHadithAdapter
    private lateinit var searchResponseAlQuranAdapter: SearchResponseAlQuranAdapter

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val bgScope = CoroutineScope(Dispatchers.IO)

    override fun onReadyForSpeech(params: Bundle?) { }

    override fun onRmsChanged(rmsdB: Float) { }

    override fun onBufferReceived(buffer: ByteArray?) { }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
        bgScope.launch {
            partialText?.let {
                val translatedText = googleTranslate.translateText(partialText, viewModel.translateLanguageCode)
                viewModel.setSpeechAndTranslationText(partialText, translatedText)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) { }

    override fun onBeginningOfSpeech() { }

    override fun onEndOfSpeech() { }

    override fun onError(error: Int) {
        viewModel.onVoiceFinished(true)
    }

    override fun onResults(results: Bundle?) {
        if (results != null) {
            val spokenTexts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//            val confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
//            Log.d("Results", spokenTexts?.toString() + " abc")
//            Log.d("Results", confidences?.contentToString() + " abc")
            spokenTexts?.let {
                val speechText = it[0]
                bgScope.launch {
                    val translatedText = googleTranslate.translateText(speechText, viewModel.translateLanguageCode)
                    val searchHits = searcher.search(speechText, translatedText)
                    viewModel.setSpeechAndTranslationText(speechText, translatedText)
                    setRecyclerViewSearchData(searchHits)
                }
            }
        }

//        Handler().postDelayed({ mSpeechRecognizeening(intent) }, 3000)
        viewModel.onVoiceFinished()
    }

    private suspend fun setRecyclerViewSearchData(searchHits: List<HitAlQuran>) {
        // TODO async search data
        withContext(Dispatchers.Main) {
            if (searchHits.isNotEmpty()) {
                viewModel.appearInLabelText.set("Appear In: ")
                viewModel.searchRecyclerViewVisibility.set(true)
                if (!::searchResponseAlQuranAdapter.isInitialized) {
                    //        SearchResponseHadithAdapter(searchHits, gson)
                    searchResponseAlQuranAdapter = SearchResponseAlQuranAdapter(searchHits, gson)
                    rvSearch.adapter = searchResponseAlQuranAdapter
                } else
                    searchResponseAlQuranAdapter.setData(searchHits)
            } else {
                viewModel.appearInLabelText.set("Appear In: None")
                viewModel.searchRecyclerViewVisibility.set(false)
            }
        }
    }
}