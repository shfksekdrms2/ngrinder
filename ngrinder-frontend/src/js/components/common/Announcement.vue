<template>
    <div class="container">
        <div v-if="announcement" id="announcement-container">
            <div class="alert alert-block">
                <div class="border-bottom">
                    <span>
                        <span v-if="ngrinder.config.hasNewAnnouncement" class="badge badge-danger" v-text="'new'"></span>
                        <span class="announcement-title" v-text="i18n('announcement.title')"></span>
                        <span class="clickable pull-right" id="hide-announcement" @click.prevent="toggleDisplay">
                            <i id="announcement-icon" class="glyphicon glyphicon-search" :class="{'oi-plus': hide, 'oi-minus': !hide}"></i>
                        </span>
                    </span>
                </div>
                <transition name="fade">
                    <div v-if="!hide" id="announcement-content" v-html="announcement"></div>
                </transition>
            </div>
        </div>
    </div>
</template>

<script>
    import { Mixins } from 'vue-mixin-decorator';
    import Component from 'vue-class-component';
    import Base from '../Base.vue';
    import MessagesMixin from '../common/mixin/MessagesMixin.vue';

    @Component({
        name: 'announcement',
    })
    export default class Announcement extends Mixins(Base, MessagesMixin) {
        ANNOUNCEMENT_HIDE_SESSION_KEY = 'announcement_hide';

        announcement = '';
        hide = false;

        created() {
            this.getAnnouncement();
            this.hide = this.$session.has(this.ANNOUNCEMENT_HIDE_SESSION_KEY) ? this.$session.get(this.ANNOUNCEMENT_HIDE_SESSION_KEY) : false;

            this.$EventBus.$on(this.$Event.CHANGE_ANNOUNCEMENT, newContent => {
                this.setAnnouncement(newContent);
                if (this.hide) {
                    this.toggleDisplay();
                }
            });
        }

        getAnnouncement() {
            this.$http.get('/announcement/api')
                .then(res => this.setAnnouncement(res.data))
                .catch(() => this.showErrorMsg(this.i18n('common.message.loading.error', { content: this.i18n('announcement.title') })));
        }

        toggleDisplay() {
            this.hide = !this.hide;
            this.$session.set(this.ANNOUNCEMENT_HIDE_SESSION_KEY, this.hide);
        }

        setAnnouncement(announcement) {
            this.announcement = announcement.replace(/\n/g, '<br>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;');
        }
    }
</script>

<style lang="less" scoped>
    .container {
        padding: 40px 0 0 0;

        .alert-block {
            color: #c09853;
            padding: 5px 20px;
            margin-bottom: 0;
            text-shadow: 0 1px 0 rgba(255, 255, 255, 0.5);
            background-color: #fcf8e3;
            border: 1px solid #fbeed5;
            border-radius: 4px;

            .border-bottom {
                margin: 0;
                padding-bottom: 2px;

                .announcement-title {
                    margin-top: 0;
                    margin-bottom: 0;
                    font-size: 15px;
                }
            }
        }

        #announcement-content {
            margin-top: 10px;
        }
    }
</style>
