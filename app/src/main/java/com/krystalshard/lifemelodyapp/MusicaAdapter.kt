package com.krystalshard.lifemelodyapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.krystalshard.lifemelodyapp.databinding.ListItemMusicaBinding

class MusicaAdapter(
    private val items: List<ListItem>,
    private val listener: OnSongClickListener,
    private val longClickListener: OnSongLongClickListener,
    private var isMultiSelectMode: Boolean = false,
    private val selectedSongs: MutableSet<Song> = mutableSetOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val SONG_VIEW_TYPE = 0
        private const val HUAWEI_AD_VIEW_TYPE = 1
        private const val META_AD_VIEW_TYPE = 2
        private const val ADMOB_AD_VIEW_TYPE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ListItem.SongItem -> SONG_VIEW_TYPE
            is ListItem.AdItem -> {
                when (item.ad) {
                    is com.facebook.ads.NativeAd -> META_AD_VIEW_TYPE
                    is com.google.android.gms.ads.nativead.NativeAd -> ADMOB_AD_VIEW_TYPE
                    else -> HUAWEI_AD_VIEW_TYPE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SONG_VIEW_TYPE -> MusicaViewHolder(ListItemMusicaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            HUAWEI_AD_VIEW_TYPE -> AdViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_ad, parent, false))
            META_AD_VIEW_TYPE -> MetaAdViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_ad_meta, parent, false))
            ADMOB_AD_VIEW_TYPE -> AdMobViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_ad_admob, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val themeColor = holder.itemView.context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("theme_color", "#C34B92") ?: "#C34B92"

        when (holder) {
            is MusicaViewHolder -> {
                val song = (items[position] as ListItem.SongItem).song
                holder.bind(song)

                if (selectedSongs.contains(song)) {
                    val colorInt = try { Color.parseColor(themeColor) } catch (e: Exception) { Color.parseColor("#C34B92") }
                    holder.itemView.setBackgroundColor((colorInt and 0x00FFFFFF) or 0x45000000)
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                }

                holder.itemView.setOnClickListener {
                    if (isMultiSelectMode) toggleSelection(song) else listener.onSongClick(song)
                }
                holder.itemView.setOnLongClickListener {
                    if (!isMultiSelectMode) {
                        longClickListener.onSongLongClick(song)
                        toggleSelection(song)
                        true
                    } else false
                }
            }
            is AdViewHolder -> {
                val adItem = items[position] as ListItem.AdItem
                holder.bind(adItem.ad as com.huawei.hms.ads.nativead.NativeAd, themeColor)
            }
            is MetaAdViewHolder -> {
                val adItem = items[position] as ListItem.AdItem
                holder.bind(adItem.ad as com.facebook.ads.NativeAd, themeColor)
            }
            is AdMobViewHolder -> {
                val adItem = items[position] as ListItem.AdItem
                holder.bind(adItem.ad as com.google.android.gms.ads.nativead.NativeAd, themeColor)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        if (!enabled) selectedSongs.clear()
        notifyDataSetChanged()
    }

    fun isInMultiSelectMode(): Boolean = isMultiSelectMode
    fun getSelectedSongs(): List<Song> = selectedSongs.toList()

    private fun toggleSelection(song: Song) {
        if (selectedSongs.contains(song)) selectedSongs.remove(song) else selectedSongs.add(song)

        val count = selectedSongs.size
        val fragment = listener as? MusicaFragment
        val tvCount = fragment?.view?.findViewById<TextView>(R.id.tvSelectionCount)

        tvCount?.let { textView ->
            if (count > 0) {
                textView.text = fragment.resources.getQuantityString(R.plurals.songs_selected_count, count, count)
            } else {
                textView.text = fragment.getString(R.string.text_select)
            }
        }
        notifyDataSetChanged()
    }

    class MusicaViewHolder(private val binding: ListItemMusicaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.songTitleText.text = song.title
            binding.songArtistText.text = song.artist
        }
    }

    class AdViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val huaweiContainer: com.huawei.hms.ads.nativead.NativeView = view.findViewById(R.id.native_ad_view)

        fun bind(ad: Any, themeColor: String) {
            if (ad is com.huawei.hms.ads.nativead.NativeAd) {
                val colorInt = try { Color.parseColor(themeColor) } catch (e: Exception) { Color.parseColor("#C34B92") }

                val title: TextView = view.findViewById(R.id.ad_title_huawei)
                val media: com.huawei.hms.ads.nativead.MediaView = view.findViewById(R.id.ad_media_huawei)
                val icon: ImageView = view.findViewById(R.id.ad_icon_huawei)
                val cta: Button = view.findViewById(R.id.ad_cta_huawei)

                cta.backgroundTintList = ColorStateList.valueOf(colorInt)

                title.text = ad.title
                cta.text = ad.callToAction
                icon.setImageDrawable(ad.icon?.drawable)

                huaweiContainer.titleView = title
                huaweiContainer.mediaView = media
                huaweiContainer.iconView = icon
                huaweiContainer.callToActionView = cta

                huaweiContainer.setNativeAd(ad)
                huaweiContainer.visibility = View.VISIBLE
            } else {
                huaweiContainer.visibility = View.GONE
            }
        }
    }

    class MetaAdViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val metaContainer: com.facebook.ads.NativeAdLayout = view.findViewById(R.id.native_ad_view_meta)
        private val adTitle: TextView = view.findViewById(R.id.ad_title_meta)
        private val adMedia: com.facebook.ads.MediaView = view.findViewById(R.id.ad_media_meta)
        private val adIcon: com.facebook.ads.MediaView = view.findViewById(R.id.ad_icon_meta)
        private val adCTA: Button = view.findViewById(R.id.ad_cta_meta)
        private val adOptionsContainer: android.widget.FrameLayout = view.findViewById(R.id.ad_options_container)

        fun bind(ad: com.facebook.ads.NativeAd, themeColor: String) {
            val colorInt = try { Color.parseColor(themeColor) } catch (e: Exception) { Color.parseColor("#C34B92") }
            adCTA.backgroundTintList = ColorStateList.valueOf(colorInt)

            adTitle.text = ad.advertiserName
            adCTA.text = ad.adCallToAction
            adCTA.visibility = if (ad.hasCallToAction()) View.VISIBLE else View.INVISIBLE

            adOptionsContainer.removeAllViews()
            val adOptionsView = com.facebook.ads.AdOptionsView(view.context, ad, metaContainer)
            adOptionsContainer.addView(adOptionsView)

            ad.unregisterView()
            val clickableViews = mutableListOf<View>(adTitle, adCTA)
            ad.registerViewForInteraction(metaContainer, adMedia, adIcon, clickableViews)
        }
    }

    class AdMobViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val adMobContainer: com.google.android.gms.ads.nativead.NativeAdView = view.findViewById(R.id.native_ad_view_admob)

        fun bind(ad: com.google.android.gms.ads.nativead.NativeAd, themeColor: String) {
            val colorInt = try { Color.parseColor(themeColor) } catch (e: Exception) { Color.parseColor("#C34B92") }

            val adTitle: TextView = view.findViewById(R.id.ad_title_admob)
            val adMedia: com.google.android.gms.ads.nativead.MediaView = view.findViewById(R.id.ad_media_admob)
            val adCTA: Button = view.findViewById(R.id.ad_cta_admob)
            val adIcon: ImageView = view.findViewById(R.id.ad_icon_admob)

            adCTA.backgroundTintList = ColorStateList.valueOf(colorInt)

            adTitle.text = ad.headline
            adCTA.text = ad.callToAction

            adMobContainer.headlineView = adTitle
            adMobContainer.mediaView = adMedia
            adMobContainer.callToActionView = adCTA
            adMobContainer.iconView = adIcon

            if (ad.icon != null) {
                adIcon.setImageDrawable(ad.icon?.drawable)
                adIcon.visibility = View.VISIBLE
            } else {
                adIcon.visibility = View.GONE
            }

            adMobContainer.setNativeAd(ad)
        }
    }

    fun actualizarColores() {
        notifyDataSetChanged()
    }
}