/**
 * 桌台预约：日期、人数、桌型、时段与联系人；带 reserveNo 时进入改期模式
 * @author come-often
 * @date 2026-11-09
 */
import {
  createReservation,
  getReservationDetail,
  getReservationMeta,
  getReservationSlots,
  rescheduleReservation,
} from '../../../api/reservationApi';

const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const MIN_PEOPLE = 1;
const MAX_PEOPLE = 20;

Page({
  data: {
    mode: 'create',
    reserveNo: '',
    storeId: '',
    meta: null,
    dates: [],
    selectedDate: '',
    peopleCount: 2,
    types: [],
    selectedType: '',
    timeSlots: [],
    allSlots: [],
    slotsEmpty: false,
    selectedTimeSlot: '',
    form: {
      contactName: '',
      contactPhone: '',
      remark: '',
    },
    cancelLimitHours: 2,
    noShowMinutes: 15,
    autoConfirm: true,
    loading: false,
    submitting: false,
  },

  async onLoad(query) {
    await getApp().globalData.loginReady;
    const reserveNo = query.reserveNo ?? '';
    this.setData({
      mode: reserveNo ? 'reschedule' : 'create',
      reserveNo,
      storeId: query.storeId ?? '',
    });
    await this.loadMeta();
    if (reserveNo) {
      await this.loadExisting(reserveNo);
    }
    await this.loadSlots();
  },

  async loadMeta() {
    try {
      const meta = await getReservationMeta();
      const dates = meta.dates ?? [];
      this.setData({
        meta,
        dates,
        selectedDate: this.data.selectedDate || dates[0] || '',
        cancelLimitHours: meta.cancelLimitHours,
        noShowMinutes: meta.noShowMinutes,
        autoConfirm: meta.autoConfirm,
      });
      this.refreshTypes();
    } catch (error) {
      wx.showToast({ title: error.message ?? '预约信息加载失败', icon: 'none' });
    }
  },

  async loadExisting(reserveNo) {
    try {
      const detail = await getReservationDetail(reserveNo);
      this.setData({
        selectedDate: detail.reserveDate,
        selectedType: detail.tableType,
        peopleCount: detail.peopleCount,
        form: {
          contactName: detail.contactName,
          contactPhone: detail.contactPhone,
          remark: detail.remark ?? '',
        },
      });
      this.refreshTypes();
    } catch (error) {
      wx.showToast({ title: error.message ?? '预约详情加载失败', icon: 'none' });
    }
  },

  refreshTypes() {
    const meta = this.data.meta;
    if (!meta) {
      return;
    }
    const people = this.data.peopleCount;
    const types = (meta.tableTypes ?? []).map((type) => ({
      code: type.code,
      label: type.name,
      tableCount: type.tableCount,
      disabled: type.tableCount <= 0 || people < type.minPeople || people > type.maxPeople,
    }));
    const current = types.find((type) => type.code === this.data.selectedType);
    let selectedType = current && !current.disabled ? current.code : '';
    if (!selectedType && types.length > 0) {
      const first = types.find((type) => !type.disabled);
      selectedType = first ? first.code : '';
    }
    this.setData({ types, selectedType });
    this.buildTimeSlots();
  },

  async loadSlots() {
    if (!this.data.selectedDate) {
      return;
    }
    this.setData({ loading: true });
    try {
      const slots = await getReservationSlots(this.data.selectedDate);
      const allSlots = slots ?? [];
      this.setData({ allSlots, slotsEmpty: allSlots.length === 0 });
      this.buildTimeSlots();
    } catch (error) {
      wx.showToast({ title: error.message ?? '时段加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  buildTimeSlots() {
    const slots = this.data.allSlots ?? [];
    const type = this.data.selectedType;
    const timeSlots = (this.data.meta?.timeSlots ?? []).map((timeSlot) => {
      const slot = slots.find((item) => item.timeSlot === timeSlot && item.tableType === type);
      const remaining = slot ? (slot.remaining ?? 0) : 0;
      const full = !slot || remaining <= 0;
      return {
        timeSlot,
        remaining,
        full,
        remainText: slot ? (full ? '已满' : `余 ${remaining}`) : '不可约',
      };
    });
    const selected = timeSlots.find((item) => item.timeSlot === this.data.selectedTimeSlot);
    this.setData({
      timeSlots,
      selectedTimeSlot: selected && !selected.full ? selected.timeSlot : '',
    });
  },

  onSelectDate(event) {
    const { date } = event.currentTarget.dataset;
    if (date === this.data.selectedDate) {
      return;
    }
    this.setData({ selectedDate: date, selectedTimeSlot: '' }, () => {
      this.loadSlots();
    });
  },

  onStepPeople(event) {
    const delta = Number(event.currentTarget.dataset.delta);
    const next = this.data.peopleCount + delta;
    if (next < MIN_PEOPLE || next > MAX_PEOPLE) {
      return;
    }
    this.setData({ peopleCount: next }, () => {
      this.refreshTypes();
    });
  },

  onInputPeople(event) {
    const value = Number(event.detail.value);
    if (Number.isNaN(value)) {
      return;
    }
    // 越界时回写合法值，避免输入框显示 25 而实际提交的仍是旧值
    const next = Math.min(MAX_PEOPLE, Math.max(MIN_PEOPLE, value));
    this.setData({ peopleCount: next }, () => {
      this.refreshTypes();
    });
  },

  onSelectType(event) {
    const { code, disabled } = event.currentTarget.dataset;
    if (disabled) {
      wx.showToast({ title: '当前人数与桌型不匹配', icon: 'none' });
      return;
    }
    this.setData({ selectedType: code, selectedTimeSlot: '' }, () => {
      this.buildTimeSlots();
    });
  },

  onSelectTimeSlot(event) {
    const { slot, full } = event.currentTarget.dataset;
    if (full) {
      return;
    }
    this.setData({ selectedTimeSlot: slot });
  },

  onInputContact(event) {
    const { field } = event.currentTarget.dataset;
    this.setData({ [`form.${field}`]: event.detail.value });
  },

  async onSubmit() {
    const { selectedDate, selectedType, selectedTimeSlot, peopleCount, form } = this.data;
    if (!selectedDate) {
      wx.showToast({ title: '请选择预约日期', icon: 'none' });
      return;
    }
    if (!selectedType) {
      wx.showToast({ title: '请选择桌型', icon: 'none' });
      return;
    }
    if (!selectedTimeSlot) {
      wx.showToast({ title: '请选择时段', icon: 'none' });
      return;
    }
    if (!form.contactName.trim()) {
      wx.showToast({ title: '请填写联系人', icon: 'none' });
      return;
    }
    if (!PHONE_PATTERN.test(form.contactPhone.trim())) {
      wx.showToast({ title: '请填写正确的手机号', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      if (this.data.mode === 'reschedule') {
        await rescheduleReservation(this.data.reserveNo, {
          reserveDate: selectedDate,
          timeSlot: selectedTimeSlot,
          tableType: selectedType,
        });
      } else {
        await createReservation({
          reserveDate: selectedDate,
          timeSlot: selectedTimeSlot,
          tableType: selectedType,
          peopleCount,
          contactName: form.contactName.trim(),
          contactPhone: form.contactPhone.trim(),
          remark: form.remark.trim(),
        });
      }
      wx.showToast({ title: this.data.mode === 'reschedule' ? '改期成功' : '预约成功', icon: 'success' });
      setTimeout(() => {
        wx.redirectTo({ url: '/packageMember/pages/reservation-list/reservation-list' });
      }, 800);
    } catch (error) {
      wx.showToast({ title: error.message ?? '提交失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
