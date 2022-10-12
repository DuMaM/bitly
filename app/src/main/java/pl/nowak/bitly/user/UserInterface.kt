package pl.nowak.bitly.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pl.nowak.bitly.R

class UserInterface : Fragment() {

    companion object {
        fun newInstance() = UserInterface()
    }

    private lateinit var viewModel: UserInterfaceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_interface, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(UserInterfaceViewModel::class.java)
        // TODO: Use the ViewModel
    }

}