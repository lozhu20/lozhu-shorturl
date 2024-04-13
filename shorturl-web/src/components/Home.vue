<template>
  <div class="content">
    <div class="main-content">
      <h2>Lozhu 的在线短链服务</h2>
      <h4>累计生成短链: {{ historyData.length }}</h4>
      <div class="input-group mb-3" style="margin-top: 40px;">
        <input v-model="URL" type="text" class="form-control" placeholder="请输入链接，一次一个哦～">
        <button v-on:click="shortenURL" class="btn btn-outline-secondary" type="button" id="button-addon2">🚀
          生成短链</button>
      </div>
      <div v-if="errorMessage" class="alert alert-danger fade show" role="alert">
        {{ errorMessage }}
      </div>
      <div v-if="warningMessage" class="alert alert-warning fade show" role="alert">
        {{ warningMessage }}
      </div>
      <div style="margin-top: 50px;">
        <table class="table table-hover">
          <thead>
            <tr>
              <th>序号</th>
              <th>原始链接</th>
              <th>短链</th>
              <th>生成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in historyData" :key="index">
              <td>{{ index + 1 }}</td>
              <td>
                <a :href=item.URL target="_blank" rel="noopener noreferrer">{{ item.URL }}</a>
              </td>
              <td>
                <a :href=item.shortURL target="_blank" rel="noopener noreferrer">{{ item.shortURL }}</a>
              </td>
              <td>{{ item.generateTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="footer">
      <p>
        <span>
          📝 <a :href="documentURL" target="_blank" rel="noopener noreferrer"> 项目文档</a>
        </span>
        |
        <span>
          <a :href="sourceCodeURL" target="_blank" rel="noopener noreferrer">源码</a>
        </span>
      </p>
      <p>
        <span>
          <div class="heart"></div>
        </span>
        <span>已勉强运行 {{ runningDays }} 天</span>
      </p>
      <p>©️ 2024 lozhu 保留所有权利</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      URL: '',
      shortURL: '',
      errorMessage: '',
      warningMessage: '',
      totalCount: 0,
      runningDays: 0,
      historyData: [],
      documentURL: 'https://lozhu.happy365.day',
      sourceCodeURL: 'https://lozhu.happy365.day'
    }
  },
  created() {
    let nowTime = new Date().getTime() / 1000
    // 开始运行时间: 2024-04-12
    let startTime = new Date(2024, 3, 12).getTime() / 1000
    let days = (nowTime - startTime) / (60 * 60 * 24)
    this.runningDays = parseInt(days + '')
  },
  methods: {
    shortenURL() {
      this.warningMessage = ''
      this.errorMessage = ''
      if (this.URL === '') {
        this.warningMessage = '请先输入链接哦～'
        return false
      }
      this.$axios.get('/api/v1/shortenURL', {
        params: {
          URL: this.URL,
          sign: ''
        }
      }).then((res) => {
        if (res.data && res.data.code === 0) {
          let tableRow = {
            URL: this.URL,
            shortURL: res.data.data,
            generateTime: this.formatDateTime(new Date())
          }
          this.historyData.push(tableRow)
        } else {
          this.errorMessage = res.data.message
        }
        this.URL = ''
      }).catch((err) => {
        this.errorMessage = '当前服务不可用'
        this.warningMessage = ''
      })
    },
    formatDateTime(date) {
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = date.getHours();
      const minute = date.getMinutes();
      const second = date.getSeconds();
      return `${year}-${this.pad(month)}-${this.pad(day)} ${this.pad(hour)}:${this.pad(minute)}:${this.pad(second)}`;
    },
    pad(num) {
      return num.toString().padStart(2, '0');
    }
  }
}
</script>

<style scoped>
.content {
  width: 80%;
  max-width: 1000px;
  margin: 60px auto;
}

h2,
h3,
h4 {
  margin-top: 30px;
}

.form-control {
  line-height: 2;
}

a {
  color: #2c3e50;
}

.footer {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  margin: 25px 0;
  text-align: center;
  line-height: 1.2;
}

th {
  color: #2c3e50;
}

.heart,
.heart::before,
.heart::after {
  content: '';
  display: block;
  width: 11px;
  height: 11px;
  border-radius: 10%;
  background-color: red;
}

.heart {
  display: inline-block;
  margin-right: 6px;
}

.heart {
  transform: rotate(45deg);
  position: relative;
  animation: heart 1s infinite;
}

.heart::before {
  position: absolute;
  left: -6px;
  border-radius: 50%;
}

.heart::after {
  position: absolute;
  top: -6px;
  border-radius: 50%;
}

@keyframes heart {
  0% {
    transform: rotate(45deg) scale(1);
  }

  20% {
    transform: rotate(45deg) scale(1.1);
  }

  90% {
    transform: rotate(45deg) scale(1);
  }

  100% {
    transform: rotate(45deg) scale(1);
  }
}
</style>
