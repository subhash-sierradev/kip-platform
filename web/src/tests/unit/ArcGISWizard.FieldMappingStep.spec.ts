// /* eslint-disable simple-import-sort/imports */
// import { mount } from '@vue/test-utils';
// import { describe, it, expect, vi } from 'vitest';
// import { nextTick } from 'vue';
// import FieldMappingStep from '@/components/outbound/arcgisintegration/wizard/steps/FieldMappingStep.vue';

// // Provide ArcGIS fields so target dropdown has options
// vi.mock('@/composables/useArcgisFeatures', () => ({
//   useArcgisFeatures: () => ({
//     fields: { value: [ { name: 'Email Id', nullable: true } ] },
//     load: vi.fn()
//   })
// }));

// describe('ArcGIS Wizard - FieldMappingStep', () => {
//   it('shows validation errors for incomplete mapping and becomes valid when completed', async () => {
//     const wrapper = mount(FieldMappingStep, {
//       props: { modelValue: { fieldMappings: [{ sourceField: '', targetField: '', isMandatory: false }] } },
//     });

//     // Initial validation-change should be false
//     await nextTick();
//     let events = wrapper.emitted('validation-change');
//     expect(events).toBeTruthy();
//     expect(events![events!.length - 1][0]).toBe(false);

//     // Fill both selects
//     const selects = wrapper.findAll('select.fm-input');
//     await selects[0].setValue('name');
//     await selects[1].setValue('Email Id');
//     await nextTick();

//     events = wrapper.emitted('validation-change');
//     expect(events).toBeTruthy();
//     expect(events![events!.length - 1][0]).toBe(true);
//   });

//   it('can add and remove mapping rows', async () => {
//     const wrapper = mount(FieldMappingStep, {
//       props: { modelValue: { fieldMappings: [{ sourceField: 'name', targetField: 'Email Id', isMandatory: false }] } },
//     });
//     await nextTick();
//     // Add a new mapping
//     const addBtn = wrapper.find('.fm-btn-add');
//     await addBtn.trigger('click');
//     await nextTick();
//     expect(wrapper.findAll('.fm-mapping-row').length).toBe(2);
//     // Remove the newly added mapping
//     const removeBtn = wrapper.findAll('.fm-btn-remove')[0];
//     await removeBtn.trigger('click');
//     await nextTick();
//     expect(wrapper.findAll('.fm-mapping-row').length).toBe(1);
//   });
// });
