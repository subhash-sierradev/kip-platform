<template>
  <div class="ms-multiuser" ref="multiUserRef">
    <div
      class="ms-multiuser-trigger"
      ref="multiUserTriggerRef"
      role="button"
      tabindex="0"
      aria-haspopup="dialog"
      :aria-expanded="open"
      @click.stop="toggleUserDropdown"
      @keydown.enter.prevent="toggleUserDropdown"
      @keydown.space.prevent="toggleUserDropdown"
      @keydown.esc.prevent="closeUserDropdown"
    >
      <span v-if="!selectedUserNames.length" class="placeholder">Select users</span>
      <span v-else class="value">{{ selectedUserNames.join(', ') }}</span>
      <span class="chevron">▾</span>
    </div>

    <div v-if="open" class="ms-multiuser-dropdown" :class="placement" ref="multiUserDropdownRef">
      <input
        type="text"
        class="ms-multiuser-search"
        v-model="userSearch"
        placeholder="Search users..."
        @keydown.esc="closeUserDropdown"
      />
      <div class="ms-multiuser-list">
        <label v-for="u in filteredUsers" :key="u.accountId" class="ms-multiuser-item">
          <input
            type="checkbox"
            :checked="isUserSelected(u.accountId)"
            @change="emit('toggle-user', u.accountId)"
          />
          <span>{{ u.displayName }}</span>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import {
  computeSelectedUserNames,
  filterUsers as filterUsersHelper,
} from '@/components/outbound/jirawebhooks/utils/CustomFieldHelper';
import {
  decidePlacementInContainer,
  type DropdownPlacement,
} from '@/components/outbound/jirawebhooks/utils/dropdownPlacementUtils';

const props = defineProps<{
  open: boolean;
  placement: DropdownPlacement;
  selectedValue: string;
  users: JiraUser[];
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'update:placement', value: DropdownPlacement): void;
  (e: 'toggle-user', accountId: string): void;
}>();

const userSearch = ref('');
const multiUserRef = ref<HTMLElement | null>(null);
const multiUserTriggerRef = ref<HTMLElement | null>(null);
const multiUserDropdownRef = ref<HTMLElement | null>(null);

const selectedUserNames = computed(() =>
  computeSelectedUserNames(props.selectedValue, props.users)
);
const filteredUsers = computed(() => filterUsersHelper(props.users, userSearch.value));

function isUserSelected(accountId: string) {
  return (props.selectedValue || '').split(',').includes(accountId);
}

function decideUserDropdownPlacement() {
  const container = multiUserRef.value?.closest('.jw-content') as HTMLElement | null;
  const placement = decidePlacementInContainer(
    multiUserTriggerRef.value,
    multiUserDropdownRef.value,
    container
  );
  emit('update:placement', placement);
}

function toggleUserDropdown() {
  const nextOpen = !props.open;
  emit('update:open', nextOpen);
}

function closeUserDropdown() {
  emit('update:open', false);
}

function onClickOutside(event: MouseEvent) {
  if (!props.open) {
    return;
  }

  if (multiUserRef.value && !multiUserRef.value.contains(event.target as Node)) {
    emit('update:open', false);
  }
}

watch(
  () => props.open,
  open => {
    if (open) {
      nextTick(() => {
        decideUserDropdownPlacement();
      });
      return;
    }

    userSearch.value = '';
  }
);

onMounted(() => {
  document.addEventListener('click', onClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside);
});
</script>
