<template>
  <ServiceConnectionPage :service-type="ServiceType.JIRA" />
</template>

<script setup lang="ts">
import ServiceConnectionPage from '@/components/common/ServiceConnectionPage.vue';
import { ServiceType } from '@/api/models/enums';
</script>
