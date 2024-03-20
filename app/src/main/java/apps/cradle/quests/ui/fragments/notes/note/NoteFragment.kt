package apps.cradle.quests.ui.fragments.notes.note

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.ui.dialogs.SlideToPerformDialog
import apps.cradle.quests.utils.ComposeUtils
import apps.cradle.quests.utils.NotesUtils
import apps.cradle.quests.utils.TextUtils
import apps.cradle.quests.utils.createSnackbar

class NoteFragment : Fragment() {

    private val noteVM by viewModels<NoteViewModel>()
    private lateinit var noteId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                NoteScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteId = arguments?.getString(EXTRA_NOTE_ID, null)
            ?: throw IllegalArgumentException("noteId is null.")
        noteVM.loadNote(noteId)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NoteScreen() {
        val note = noteVM.noteFlow.collectAsStateWithLifecycle(initialValue = null)
        val windowBackgroundColor = Color(
            ContextCompat.getColor(requireActivity(), R.color.windowBackground)
        )
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = note.value?.questTitle.orEmpty(),
                            fontSize = ComposeUtils.TextSize.LARGE
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = stringResource(R.string.buttonBack)
                            )
                        }
                    },
                    actions = {
                        if (note.value?.isArchived == false) {
                            IconButton(onClick = onEditNoteClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit),
                                    contentDescription = stringResource(R.string.buttonEdit)
                                )
                            }
                            IconButton(onClick = onDeleteNoteClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.buttonDelete)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = windowBackgroundColor,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            containerColor = windowBackgroundColor,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Column {
                        Text(
                            text = note.value?.title.orEmpty(),
                            color = windowBackgroundColor,
                            fontSize = ComposeUtils.TextSize.HUGE,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        val content = remember {
                            TextUtils.getAnnotatedString(note.value?.content.orEmpty())
                        }
                        ClickableText(
                            modifier = Modifier.padding(16.dp),
                            text = content,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = ComposeUtils.TextSize.LARGE
                            )
                        ) { index ->
                            content.getStringAnnotations(index, index).lastOrNull()?.let {
                                onLinkClicked(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onLinkClicked(annotation: AnnotatedString.Range<String>) {
        val intent = when (annotation.tag) {
            TextUtils.TAG_WEB_URL -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
            }

            TextUtils.TAG_EMAIL_ADDRESS -> {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(annotation.item))
                }
            }

            else -> null
        }
        intent?.run {
            try {
                startActivity(this)
            } catch (exc: Exception) {
                view?.let {
                    createSnackbar(
                        anchorView = it,
                        text = getString(R.string.snackbarNoActivity)
                    ).show()
                }
            }
        }
    }

    private val onEditNoteClicked: () -> Unit = {
        val bundle = Bundle()
        bundle.putString(EXTRA_NOTE_ID, noteId)
        findNavController().navigate(R.id.action_noteFragment_to_editNoteFragment, bundle)
    }

    private val onDeleteNoteClicked: () -> Unit = {
        SlideToPerformDialog(
            titleText = getString(R.string.deleteNoteDialogTitle),
            messageText = getString(R.string.deleteNoteDialogMessage),
            action = getString(R.string.delete),
            onSliderFinishedListener = {
                NotesUtils.deleteNote(noteId)
                findNavController().popBackStack()
            }
        ).show(childFragmentManager, "delete_note_dialog")
    }

    companion object {

        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}