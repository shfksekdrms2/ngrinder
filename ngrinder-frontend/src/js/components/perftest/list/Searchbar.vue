<template>
    <div class="search-bar">
        <div class="left-float">
            <div data-step="3" data-position="top" :data-intro="i18n('intro.list.search')">
                <select2 v-model="selectedTag" :value="selectedTag" @change="$emit('change-tag')"
                         :option="{placeholder: i18n('perfTest.action.selectATag'), allowClear: true}">
                    <option value=""></option>
                    <option v-for="tag in userTags" v-text="tag" :value="tag"></option>
                </select2>
                <input type="search" name="search" class="search-query search-query-without-radios form-control"
                       placeholder="Keywords" v-model="searchText">
                <button class="mb-1 btn btn-info" @click="$emit('search')" v-text="i18n('common.button.search')">
                    <i class="glyphicon glyphicon-search"></i>
                </button>
                <label class="checkbox">
                    <input type="checkbox" @click="$emit('filter-running', {enable: !running, token: 'R'})" v-model="running">
                    <span v-text="i18n('perfTest.action.running')"></span>
                </label>
                <label class="checkbox">
                    <input type="checkbox" @click="$emit('filter-schduled', {enable: !scheduled, token: 'S'})" v-model="scheduled">
                    <span v-text="i18n('perfTest.action.scheduled')"></span>
                </label>
            </div>
        </div>
        <div class="right-float">
            <button class="btn btn-primary" @click="$emit('create')" data-position="left" data-step="1"
               :data-intro="i18n('intro.list.create')" v-text="i18n('perfTest.action.createTest')">
                <i class="glyphicon glyphicon-file icon-white"></i>
            </button>
            <button @click="$emit('delete-selected-tests')" class="pointer-cursor btn btn-danger" data-position="top"
               data-step="2" :data-intro="i18n('intro.list.delete')" v-text="i18n('perfTest.action.deleteSelectedTest')">
                <i class="glyphicon glyphicon-remove icon-white"></i>
            </button>
        </div>
    </div>
</template>
<script>
    import { Mixins } from 'vue-mixin-decorator';
    import Component from 'vue-class-component';
    import Base from '../../Base.vue';
    import Select2 from '../../common/Select2.vue';
    import MessagesMixin from '../../common/mixin/MessagesMixin.vue';

    @Component({
        name: 'searchBar',
        components: { Select2 },
    })
    export default class SearchBar extends Mixins(Base, MessagesMixin) {
        searchText = '';
        selectedTag = '';
        userTags = [];

        running = false;
        scheduled = false;

        created() {
            this.getUserTags();
        }

        getUserTags() {
            this.$http.get('/perftest/api/search_tag')
                .then(res => this.userTags = res.data)
                .catch(() => this.showErrorMsg(this.i18n('common.message.loading.error',
                    { content: this.i18n('perfTest.list.tags') })));
        }
    }
</script>

<style lang="less">
    .select2-container {
        .select2-choice {
            width: 150px;
            height: 32px;

            span {
                margin-top: 2px;
            }
        }
    }
</style>

<style lang="less" scoped>
    .search-bar {
        height: 53px;
        border-radius: 0;
        margin: 0;
        padding: 10px;
        border: 1px solid #e3e3e3;
        border-bottom: none;
        background-color: #f5f5f5;

        .form-control {
            display: inline-block;
        }

        .search-query {
            width: 140px;
        }

        .checkbox {
            position:relative;
            margin-left:5px;
        }

        * {
            font-size: 12px;
        }
    }
</style>
