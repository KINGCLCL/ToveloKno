<template>
  <svg
    class="line-icon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    v-html="iconMarkup"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
})

const icons = {
  home: '<path d="m3 11 9-8 9 8"/><path d="M5 10v10h14V10"/><path d="M9 20v-6h6v6"/>',
  list: '<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 9h8M8 13h8M8 17h5"/>',
  'plus-circle': '<circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/>',
  upload: '<path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5"/><path d="M4 14v5a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-5"/>',
  trash: '<path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5"/>',
  folder: '<path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z"/>',
  messages: '<path d="M4 5h16v11H9l-5 4Z"/><path d="M8 9h8M8 12h5"/>',
  tag: '<path d="M20 13 13 20 4 11V4h7Z"/><circle cx="8.5" cy="8.5" r="1"/>',
  'bar-chart': '<path d="M4 20V10h4v10M10 20V4h4v16M16 20v-7h4v7"/>',
  edit: '<path d="M4 20h4l11-11-4-4L4 16Z"/><path d="m13.5 6.5 4 4"/>',
  notebook: '<path d="M6 3h12v18H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"/><path d="M8 3v18M11 8h4"/>',
  star: '<path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z"/>',
  clipboard: '<path d="M9 5H6a2 2 0 0 0-2 2v13h16V7a2 2 0 0 0-2-2h-3"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M8 12h8M8 16h6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  'pie-chart': '<path d="M12 3v9h9"/><path d="M19.5 15.8A8.5 8.5 0 1 1 8.2 4.5"/>',
  alert: '<path d="M12 3 2.5 20h19Z"/><path d="M12 9v4M12 17h.01"/>',
  'thumbs-up': '<path d="M7 10v10H3V10Z"/><path d="M7 19h9.5a2 2 0 0 0 2-1.6l1.3-6A2 2 0 0 0 17.8 9H14l.7-3.2A2.3 2.3 0 0 0 12.5 3L7 10Z"/>',
  database: '<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v6c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 11v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6"/>',
  users: '<circle cx="9" cy="8" r="3"/><path d="M3 20v-2a5 5 0 0 1 10 0v2"/><circle cx="17" cy="9" r="2.5"/><path d="M15 14a4 4 0 0 1 6 3.5V20"/>',
  shield: '<path d="M12 3 4.5 6v5.5c0 4.4 3.2 7.8 7.5 9.5 4.3-1.7 7.5-5.1 7.5-9.5V6Z"/><path d="m9 12 2 2 4-4"/>',
  'book-open': '<path d="M3 5.5A3.5 3.5 0 0 1 6.5 2H11v17H6.5A3.5 3.5 0 0 0 3 22Z"/><path d="M21 5.5A3.5 3.5 0 0 0 17.5 2H13v17h4.5A3.5 3.5 0 0 1 21 22Z"/>',
  'check-square': '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="m8 12 3 3 6-7"/>',
  'x-square': '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="m9 9 6 6M15 9l-6 6"/>',
  'trending-up': '<path d="m3 17 6-6 4 4 7-8"/><path d="M15 7h5v5"/>',
  'x-circle': '<circle cx="12" cy="12" r="9"/><path d="m9 9 6 6M15 9l-6 6"/>',
  'file-text': '<path d="M6 2h8l4 4v16H6Z"/><path d="M14 2v5h5M9 12h6M9 16h6"/>',
  shuffle: '<path d="M3 7h3c5 0 5 10 10 10h5"/><path d="m18 14 3 3-3 3M3 17h3c2.5 0 3.8-2.5 5-5"/><path d="M15 7h6m-3-3 3 3-3 3"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/>',
  bell: '<path d="M6 9a6 6 0 0 1 12 0c0 7 3 6 3 8H3c0-2 3-1 3-8"/><path d="M10 20h4"/>',
  'chevron-down': '<path d="m7 10 5 5 5-5"/>',
  'chevron-right': '<path d="m10 7 5 5-5 5"/>',
  copy: '<rect x="8" y="8" width="11" height="13" rx="2"/><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h3"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  layers: '<path d="m12 3 9 5-9 5-9-5Z"/><path d="m3 12 9 5 9-5M3 16l9 5 9-5"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1A1.7 1.7 0 0 0 9 4.6 1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z"/>',
  target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
  download: '<path d="M12 4v12m0 0 5-5m-5 5-5-5"/><path d="M4 20h16"/>',
  refresh: '<path d="M20 7v5h-5"/><path d="M4 17v-5h5"/><path d="M18.2 9A7 7 0 0 0 6.4 6.4L4 9M5.8 15A7 7 0 0 0 17.6 17.6L20 15"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/>',
  eye: '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/>',
  link: '<path d="M10 13a5 5 0 0 0 7.1.1l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1"/><path d="M14 11a5 5 0 0 0-7.1-.1l-2 2A5 5 0 0 0 12 20l1.1-1.1"/>',
  lock: '<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
}

const iconMarkup = computed(() => icons[props.name] ?? icons.star)
</script>
